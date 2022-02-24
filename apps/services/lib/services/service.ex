defmodule Services.Service do
  defprotocol Hash do
    @spec compute_hash(service :: Services.t()) :: String.t()
    def compute_hash(service)
  end

  defmodule Metadata do
    use TypeCheck
    defstruct [:provider, :file]
    @type! t :: %__MODULE__{provider: String.t() | nil, file: String.t() | nil}
  end

  defmodule OpenApi do
    use TypeCheck
    alias Services.Service.Metadata
    defstruct [:id, :name, :content, use_proxy: false, metadata: %Metadata{}]

    @type! t :: %__MODULE__{
             id: String.t(),
             name: String.t(),
             content: String.t(),
             use_proxy: boolean(),
             metadata: Services.Service.Metadata.t()
           }
  end

  defmodule AsyncApi do
    use TypeCheck
    alias Services.Service.Metadata

    defstruct [:id, :name, :content, use_proxy: false, metadata: %Metadata{}]

    @type! t :: %__MODULE__{
             id: String.t(),
             name: String.t(),
             content: String.t(),
             use_proxy: boolean(),
             metadata: Services.Service.Metadata.t()
           }
  end

  defmodule Grpc do
    use TypeCheck
    alias Services.Service.Metadata

    defmodule Server do
      use TypeCheck
      defstruct [:address, :port, use_tls: false]

      @type! t :: %__MODULE__{
               address: String.t(),
               port: pos_integer(),
               use_tls: boolean()
             }
    end

    defstruct [:id, :name, :schema, servers: %{}, metadata: %Metadata{}]

    @type! t :: %__MODULE__{
             id: String.t(),
             name: String.t(),
             schema: String.t(),
             servers: map(),
             metadata: Services.Service.Metadata.t()
           }
  end

  defimpl Services.Service.Hash,
    for: [Services.Service.AsyncApi, Services.Service.Grpc, Services.Service.OpenApi] do
    alias Services.Service.{AsyncApi, Grpc, OpenApi}

    def compute_hash(%OpenApi{content: content}) do
      :crypto.hash(:sha, content) |> Base.encode16(case: :lower)
    end

    def compute_hash(%AsyncApi{content: content}) do
      :crypto.hash(:sha, content) |> Base.encode16(case: :lower)
    end

    def compute_hash(%Grpc{schema: content}) do
      :crypto.hash(:sha, content) |> Base.encode16(case: :lower)
    end
  end
end
