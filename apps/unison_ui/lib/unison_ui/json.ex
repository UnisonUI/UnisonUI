defimpl Jason.Encoder, for: [Services.Event.Up, Services.Event.Down, Services.Event.Changed] do
  alias Services.{Event, Service}

  def encode(%Event.Up{service: service}, opts),
    do: service |> encode() |> add_event(:serviceUp) |> Jason.Encode.value(opts)

  def encode(struct = %Event.Down{}, opts),
    do: struct |> Map.from_struct() |> add_event(:serviceDown) |> Jason.Encode.value(opts)

  def encode(struct = %Event.Changed{}, opts),
    do: struct |> Map.from_struct() |> add_event(:serviceChanged) |> Jason.Encode.value(opts)

  defp encode(struct = %Service.AsyncApi{use_proxy: use_proxy}),
    do:
      struct
      |> Map.from_struct()
      |> Map.take([:id, :name, :use_proxy, :metadata])
      |> add_type(:asyncapi)
      |> Map.put(:useProxy, use_proxy)
      |> Map.delete(:use_proxy)

  defp encode(struct = %Service.OpenApi{use_proxy: use_proxy}),
    do:
      struct
      |> Map.from_struct()
      |> Map.take([:id, :name, :use_proxy, :metadata])
      |> add_type(:openapi)
      |> Map.put(:useProxy, use_proxy)
      |> Map.delete(:use_proxy)

  defp encode(struct = %Service.Grpc{}),
    do:
      struct
      |> Map.from_struct()
      |> Map.take([:id, :name, :metadata])
      |> add_type(:grpc)

  defp add_event(map, type), do: map |> Map.put_new(:event, type)
  defp add_type(map, type), do: map |> Map.put_new(:type, type)
end

defimpl Jason.Encoder, for: Services.Service.Metadata do
  def encode(%Services.Service.Metadata{} = metadata, opts),
    do: metadata |> Map.from_struct() |> Jason.Encode.value(opts)
end
