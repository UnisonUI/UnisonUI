defmodule Services.State do
  alias Services.{Event, Hash}

  @type t :: %__MODULE__{services: %{String.t() => Services.t()}}
  defstruct [:services]

  @spec new() :: t()
  def new, do: %__MODULE__{services: %{}}

  @spec available_services(t()) :: [Services.Event.t()]
  def available_services(state),
    do: Enum.into(state.services, [], fn {_, service} -> %Event.Up{service: service} end)

  @spec service(state :: t(), id :: String.t()) :: Services.t() | nil
  def service(%__MODULE__{services: services}, id), do: services[id]

  @spec reduce(state :: t(), event :: Services.Event.t()) :: {t(), [Services.Event.t()]}
  def reduce(%__MODULE__{} = state, event) do
    case event do
      %Event.Up{service: %{id: id} = service} ->
        service_up = event
        service_down = %Event.Down{id: id}

        events =
          case {named_changed?(state, service), new_service?(state, service),
                content_changed?(state, service)} do
            {_, _, true} -> [%Event.Changed{id: id}]
            {true, _, _} -> [service_down, service_up]
            {_, true, _} -> [service_up]
            _ -> []
          end

        services = Map.update(state.services, id, service, fn _ -> service end)
        {%__MODULE__{services: services}, events}

      %Event.Down{id: id} = event ->
        {%__MODULE__{services: Map.delete(state.services, id)}, [event]}
    end
  end

  @spec named_changed?(t(), Services.t()) :: boolean()
  defp named_changed?(%__MODULE__{services: services}, %{id: id, name: name}),
    do:
      Enum.any?(
        services,
        &match?({^id, %{name: service_name}} when service_name != name, &1)
      )

  defp named_changed?(_, _), do: false

  @spec new_service?(t(), Services.t()) :: boolean()
  defp new_service?(%__MODULE__{services: services}, %{id: id}), do: !Map.has_key?(services, id)

  @spec content_changed?(t(), Services.t()) :: boolean()
  defp content_changed?(%__MODULE__{services: services}, service),
    do:
      Enum.any?(services, fn {id, current_service} ->
        id == service.id &&
          Hash.compute_hash(current_service) != Hash.compute_hash(service)
      end)
end
