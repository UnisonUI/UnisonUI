defmodule Services.Application do
  # See https://hexdocs.pm/elixir/Application.html
  # for more information on OTP Applications
  @moduledoc false

  use Application

  @impl true
  def start(_type, _args) do
    children = [
      {Task.Supervisor, name: Services.TaskSupervisor},
      %{
        id: Services.Aggregator,
        start: {Services.Aggregator, :start_link, []}
      }
    ]

    # See https://hexdocs.pm/elixir/Supervisor.html
    # for other strategies and supported options
    opts = [strategy: :one_for_one, name: Services.Supervisor]
    Supervisor.start_link(children, opts)
  end
end
