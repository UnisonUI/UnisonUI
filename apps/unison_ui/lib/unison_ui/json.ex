defimpl Jason.Encoder, for: [Services.Event.Up, Services.Event.Down, Services.Event.Changed] do
  alias Services.{Event, Service}

  def encode(%Event.Up{service: service}, opts),
    do: service |> encode() |> add_event(:serviceUp) |> Jason.Encode.value(opts)

  def encode(struct = %Event.Down{}, opts),
    do: struct |> Map.from_struct() |> add_event(:serviceDown) |> Jason.Encode.value(opts)

  def encode(%Event.Changed{service: service}, opts),
    do: service |> encode() |> add_event(:serviceChanged) |> Jason.Encode.value(opts)

  defp encode(struct = %Service.AsyncApi{}),
    do:
      struct
      |> Map.from_struct()
      |> Map.take([:id, :name, :use_proxy, :metadata, :content])
      |> add_type(:asyncapi)

  defp encode(struct = %Service.OpenApi{}),
    do:
      struct
      |> Map.from_struct()
      |> Map.take([:id, :name, :use_proxy, :metadata, :content])
      |> add_type(:openapi)

  defp encode(struct = %Service.Grpc{}),
    do:
      struct
      |> Map.from_struct()
      |> Map.take([:id, :name, :metadata, :schema, :servers])
      |> Map.update!(:servers, fn servers ->
        Enum.into(servers, [], fn {name, server} ->
          server |> Map.from_struct() |> Map.put(:name, name)
        end)
      end)
      |> add_type(:grpc)

  defp add_event(map, type), do: map |> Map.put_new(:event, type)
  defp add_type(map, type), do: map |> Map.put_new(:type, type)
end

defimpl Jason.Encoder, for: Services.Service.Metadata do
  def encode(%Services.Service.Metadata{} = metadata, opts),
    do: metadata |> Map.from_struct() |> Jason.Encode.value(opts)
end
