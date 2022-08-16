defmodule GitProvider.CleaningServer do
  use Clustering.GlobalServer, supervisor: GitProvider.Git.DynamicSupervisor
  require Logger
  require Services
  @timeout 60_000

  def init(_) do
    Services.init_wait_for_storage(nil)
  end

  Services.wait_for_storage do
    send(self(), :clean)
    {:noreply, state}
  end

  def handle_info(:clean, state) do
    running_processes =
      Horde.DynamicSupervisor.which_children(GitProvider.Git.DynamicSupervisor)
      |> Enum.flat_map(fn {_, pid, _, _} ->
        with [{Git, name}] <- Horde.Registry.keys(Clustering.Registry, pid) do
          [name]
        else
          _ -> []
        end
      end)
      |> MapSet.new()

    with {:ok, services} <- Services.available_services() do
      {current, ids} =
        services
        |> Enum.flat_map(fn
          %{metadata: %Services.Service.Metadata{provider: "git"}, id: id} ->
            [name | _] = String.split(id, ":")
            [{name, id}]

          _ ->
            []
        end)
        |> Enum.unzip()

      current =
        current
        |> Enum.uniq()
        |> MapSet.new()

      MapSet.difference(current, running_processes)
      |> Stream.flat_map(fn name ->
        Enum.filter(ids, fn id -> String.starts_with?(id, name) end)
      end)
      |> Enum.into([], &%Services.Event.Down{id: &1})
      |> Services.dispatch_events()
    end

    Process.send_after(self(), :clean, @timeout)
    {:noreply, state}
  end
end
