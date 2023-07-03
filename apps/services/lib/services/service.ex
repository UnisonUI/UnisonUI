defmodule Services.Service do
  defprotocol Hash do
    @spec compute_hash(service :: Services.t()) :: String.t()
    def compute_hash(service)
  end

  defmodule Metadata do
    use TypeCheck
    use TypeCheck.Defstruct

    defstruct!(
      provider: nil :: String.t() | nil,
      file: nil :: String.t() | nil
    )
  end

  defmodule OpenApi do
    use TypeCheck
    use TypeCheck.Defstruct
    alias Services.Service.Metadata

    defstruct!(
      id: _ :: String.t(),
      name: _ :: String.t(),
      content: _ :: String.t(),
      use_proxy: false :: boolean(),
      metadata: %Metadata{} :: Services.Service.Metadata.t()
    )
  end

  defmodule AsyncApi do
    use TypeCheck
    use TypeCheck.Defstruct
    alias Services.Service.Metadata

    defstruct!(
      id: _ :: String.t(),
      name: _ :: String.t(),
      content: _ :: String.t(),
      metadata: %Metadata{} :: Services.Service.Metadata.t()
    )
  end

  defmodule Grpc do
    use TypeCheck
    use TypeCheck.Defstruct
    alias Services.Service.Metadata

    defmodule Server do
      use TypeCheck
      use TypeCheck.Defstruct

      defstruct!(
        address: _ :: String.t(),
        port: _ :: pos_integer(),
        use_tls: false :: boolean()
      )
    end

    defstruct!(
      id: _ :: String.t(),
      name: _ :: String.t(),
      schema: _ :: String.t(),
      servers: %{} :: map(),
      metadata: %Metadata{} :: Services.Service.Metadata.t()
    )
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

    def compute_hash(%Grpc{schema: content}) when is_binary(content) do
      :crypto.hash(:sha, content) |> Base.encode16(case: :lower)
    end
  end
end
