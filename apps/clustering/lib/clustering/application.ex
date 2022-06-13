defmodule Clustering.Application do
  @moduledoc false

  use Application

  def start(_type, _args) do
    opts = [strategy: :one_for_one, name: Clustering.Supervisor]

    Application.get_env(:clustering, :provider)
    |> topologies()
    |> children()
    |> Supervisor.start_link(opts)
  end

  defp topologies("aws") do
    config = Application.get_env(:clustering, :aws, [])

    [
      strategy: ClusterEC2.Strategy.Tags,
      config: [
        ec2_tagname: Keyword.get(config, :tag_name, "app"),
        ec2_tagvalue: Keyword.get(config, :tag_value, "unisonui"),
        app_prefix: Keyword.get(config, :application_name, "unisonui")
      ]
    ]
  end

  defp topologies("gcp"),
    do: [
      strategy: Cluster.Strategy.GoogleComputeEngine,
      config: [
        release_name: Application.get_env(:clustering, :gcp, "unisonui")
      ]
    ]

  defp topologies("hosts"),
    do: [
      strategy: Cluster.Strategy.Epmd,
      config: [
        hosts:
          Application.get_env(:clustering, :hosts, [])
          |> Enum.map(fn host ->
            host = if String.contains?(host, "@"), do: host, else: "unisonui@#{host}"
            String.to_atom(host)
          end)
      ]
    ]

  defp topologies("kubernetes") do
    kw =
      Application.get_env(:clustering, :kubernetes,
        service: "unisonui",
        application_name: "unisonui",
        namespace: "default"
      )

    [
      strategy: Cluster.Strategy.Kubernetes.DNSSRV,
      config: [
        service: kw[:service] || "unisonui",
        application_name: kw[:application_name] || "unisonui",
        namespace: kw[:namespace] || "default"
      ]
    ]
  end

  defp topologies(_), do: []

  defp children([]), do: horde_children()

  defp children(topologies),
    do: [
      {Cluster.Supervisor, [[clustering: topologies], [name: UnisonUI.ClusterSupervisor]]},
      {Task.Supervisor, name: Clustering.TaskSupervisor}
      | horde_children()
    ]

  defp horde_children,
    do: [
      {Horde.Registry, [name: Clustering.Registry, keys: :unique, members: :auto]},
      {Horde.DynamicSupervisor,
       [name: Clustering.DynamicSupervisor, strategy: :one_for_one, members: :auto]}
    ]
end
