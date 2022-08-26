defmodule ConsulProvider.Application do
  # See https://hexdocs.pm/elixir/Application.html
  # for more information on OTP Applications
  @moduledoc false

  use Application

  @impl true
  def start(_type, _args) do
    opts = [strategy: :one_for_one, name: ConsulProvider.Supervisor]

    Application.fetch_env!(:consul, :enabled)
    |> child_spec()
    |> Supervisor.start_link(opts)
  end

  defp child_spec(false) do
    Logger.info("Consul provider has been disabled")
    []
  end

  defp child_spec(_) do
    [
      {Finch, name: ConsulProvider.Finch}
    ]
  end
end
