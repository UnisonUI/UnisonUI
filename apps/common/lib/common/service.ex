defprotocol Common.Service do
  @type t :: Common.Service.OpenApi.t() | Common.Service.Grpc.t()

  @spec to_event(service :: t()) :: map()
  def to_event(service)

  @spec compute_hash(service :: t()) :: String.t()
  def compute_hash(service)
end

defmodule Common.Service.Metadata do
  @derive Jason.Encoder
  @type t :: %__MODULE__{provider: String.t() | nil, file: String.t() | nil}
  defstruct [:provider, :file]
end

defmodule Common.Service.OpenApi do
  @type t :: %__MODULE__{
          id: String.t(),
          name: String.t(),
          content: String.t(),
          use_proxy: boolean(),
          metadata: Common.Service.Metadata.t()
        }
  @enforce_keys [:id, :name, :content]
  defstruct [:id, :name, :content, use_proxy: false, metadata: %Common.Service.Metadata{}]
end

defmodule Common.Service.Grpc do
  @type server :: [address: String.t(), port: pos_integer(), use_tls: boolean()]
  @type t :: %__MODULE__{
          id: String.t(),
          name: String.t(),
          schema: String.t(),
          servers: map(),
          metadata: Common.Service.Metadata.t()
        }
  @enforce_keys [:id, :name, :schema]
  defstruct [:id, :name, :schema, servers: %{}, metadata: %Common.Service.Metadata{}]
end

defimpl Common.Service, for: [Common.Service.Grpc, Common.Service.OpenApi] do
  alias Common.Service.{Grpc, OpenApi}

  def to_event(struct = %OpenApi{use_proxy: use_proxy}),
    do:
      struct
      |> Map.from_struct()
      |> Map.take([:id, :name, :use_proxy, :metadata])
      |> add_type(:openapi)
      |> Map.put(:useProxy, use_proxy)
      |> Map.delete(:use_proxy)

  def to_event(struct = %Grpc{}),
    do:
      struct
      |> Map.from_struct()
      |> Map.take([:id, :name, :metadata])
      |> add_type(:grpc)

  defp add_type(map, type), do: map |> Map.put_new(:type, type)

  def compute_hash(%OpenApi{content: content}) do
    :crypto.hash(:sha, content) |> Base.encode16(case: :lower)
  end

  def compute_hash(%Grpc{schema: content}) do
    :crypto.hash(:sha, content) |> Base.encode16(case: :lower)
  end
end
