defmodule Services.State do
  require Logger
  alias Common.Service
  alias Common.Events

  @behaviour :ra_machine

  @task Services.TaskSupervisor

  @impl true
  def init(_), do: %{}

  @impl true
  def apply(_meta, {:event, event}, services) do
    debug_event(event)

    {events, services} =
      case event do
        %Events.Up{service: %{id: id} = service} ->
          service_up = event
          service_down = %Events.Down{id: id}

          events =
            case {named_changed?(services, service), new_service?(services, service),
                  content_changed?(services, service)} do
              {_, _, true} -> [%Events.Changed{id: id}]
              {true, _, _} -> [service_down, service_up]
              {_, true, _} -> [service_up]
              _ -> []
            end

          services = Map.update(services, id, service, fn _ -> service end)
          {events, services}

        %Events.Down{id: id} = event ->
          {[event], Map.delete(services, id)}
      end

    {services, :ok, [dispatch_events(events)]}
  end

  defp debug_event(%Events.Up{service: %{id: id}}), do: Logger.debug("New up event: #{id}")
  defp debug_event(%Events.Down{id: id}), do: Logger.debug("New down event: #{id}")

  defp dispatch_events(events),
    do:
      {:mod_call, Task.Supervisor, :start_child,
       [
         @task,
         fn ->
           Node.list([:visible, :this])
           |> Enum.each(&:rpc.call(&1, Services.Aggregator, :append_events, [events]))
         end
       ]}

  @spec named_changed?(services :: %{String.t() => Service.t()}, Service.t()) :: boolean()
  defp named_changed?(services, %{id: id, name: name}),
    do:
      Enum.any?(
        services,
        &match?({^id, %{name: service_name}} when service_name != name, &1)
      )

  defp named_changed?(_, _), do: false

  @spec new_service?(services :: %{String.t() => Service.t()}, Service.t()) :: boolean()
  defp new_service?(services, %{id: id}), do: !Map.has_key?(services, id)

  @spec content_changed?(services :: %{String.t() => Service.t()}, Service.t()) :: boolean()
  defp content_changed?(services, service),
    do:
      Enum.any?(services, fn {id, current_service} ->
        id == service.id &&
          Service.compute_hash(current_service) != Service.compute_hash(service)
      end)
end
