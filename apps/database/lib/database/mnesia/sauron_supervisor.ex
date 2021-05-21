defmodule Database.Mnesia.SauronSupervisor do
  @moduledoc false
  require Logger

  def start_leader do
    _ = :global.sync()
    {:ok, pid} = Database.GlobalMonitor.monitor(Database.Mnesia.Sauron)
    Logger.debug("Sauron started on #{inspect(node(pid))}")
    :ok
  end
end
