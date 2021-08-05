defprotocol Services.Hash do
  @spec compute_hash(service :: Services.t()) :: String.t()
  def compute_hash(service)
end

defmodule Services.Metadata do
  @type t :: %__MODULE__{provider: String.t() | nil, file: String.t() | nil}
  defstruct [:provider, :file]
end

defmodule Services.OpenApi do
  @type t :: %__MODULE__{
          id: String.t(),
          name: String.t(),
          content: String.t(),
          use_proxy: boolean(),
          metadata: Services.Metadata.t()
        }
  @enforce_keys [:id, :name, :content]
  defstruct [:id, :name, :content, use_proxy: false, metadata: %Services.Metadata{}]
end

defmodule Services.Grpc do
  @type server :: [address: String.t(), port: pos_integer(), use_tls: boolean()]
  @type t :: %__MODULE__{
          id: String.t(),
          name: String.t(),
          schema: String.t(),
          servers: map(),
          metadata: Services.Metadata.t()
        }
  @enforce_keys [:id, :name, :schema]
  defstruct [:id, :name, :schema, servers: %{}, metadata: %Services.Metadata{}]
end

defimpl Services.Hash, for: [Services.Grpc, Services.OpenApi] do
  alias Services.{Grpc, OpenApi}

  def compute_hash(%OpenApi{content: content}) do
    :crypto.hash(:sha, content) |> Base.encode16(case: :lower)
  end

  def compute_hash(%Grpc{schema: content}) do
    :crypto.hash(:sha, content) |> Base.encode16(case: :lower)
  end
end
