defmodule Database.Application do
  # See https://hexdocs.pm/elixir/Application.html
  # for more information on OTP Applications
  @moduledoc false

  use Application

  def start(_type, _args) do
    mnesia_dir = :mnesia.system_info(:directory)
    _ = File.mkdir_p(mnesia_dir)

    children = [
      {Database.GlobalMonitor, []},
      %{
        id: Database.MnesiaSupervisor,
        start: {Database.MnesiaSupervisor, :start_link, []}
      }
    ]

    opts = [strategy: :one_for_one, name: Database.Supervisor]
    Supervisor.start_link(children, opts)
  end
end
