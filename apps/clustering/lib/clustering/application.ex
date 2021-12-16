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
        ec2_tagname: Keyword.fetch!(config, :tag_name),
        ec2_tagvalue: Keyword.fetch!(config, :tag_value),
        app_prefix: "unison_ui"
      ]
    ]
  end

  defp topologies("gcp"),
    do: [
      strategy: Cluster.Strategy.GoogleComputeEngine,
      config: [
        release_name: "unison_ui"
      ]
    ]

  defp topologies("hosts"),
    do: [
      strategy: Cluster.Strategy.Epmd,
      config: [
        hosts:
          Application.get_env(:clustering, :hosts, [])
          |> Enum.map(fn host ->
            host = if String.contains?(host, "@"), do: host, else: "unison_ui@#{host}"
            String.to_atom(host)
          end)
      ]
    ]

  defp topologies("kubernetes"),
    do: [
      strategy: Cluster.Strategy.Kubernetes.DNS,
      config: [
        service:
          Application.get_env(:clustering, :kubernetes, service: "unisonui")
          |> Keyword.get(:service),
        application_name: "unison_ui"
      ]
    ]

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
