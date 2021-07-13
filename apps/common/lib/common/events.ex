defmodule Common.Events do
  @type t :: Common.Events.Up.t() | Common.Events.Down.t() | Common.Events.Changed.t()
  defmodule Up do
    @type t :: %__MODULE__{service: Common.Service.t()}
    @enforce_keys [:service]
    defstruct [:service]
  end

  defmodule Down do
    @type t :: %__MODULE__{id: String.t()}
    @enforce_keys [:id]
    defstruct [:id]
  end

  defmodule Changed do
    @type t :: %__MODULE__{id: String.t()}
    @enforce_keys [:id]
    defstruct [:id]
  end

  defimpl Jason.Encoder, for: [Up, Down, Changed] do
    alias Common.Events.{Up, Down, Changed}
    alias Common.Service

    def encode(%Up{service: service}, opts),
      do: Service.to_event(service) |> add_event(:serviceUp) |> Jason.Encode.value(opts)

    def encode(struct = %Down{}, opts),
      do: struct |> Map.from_struct() |> add_event(:serviceDown) |> Jason.Encode.value(opts)

    def encode(struct = %Changed{}, opts),
      do: struct |> Map.from_struct() |> add_event(:serviceChanged) |> Jason.Encode.value(opts)

    defp add_event(map, type), do: map |> Map.put_new(:event, type)
  end
end
