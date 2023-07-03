defmodule FakeStorage do
  @behaviour Services.Storage
  alias Services.Service.{OpenApi, Metadata}

  @spec alive?() :: true
  def alive?, do: true

  @spec available_services :: {:ok, [Services.t()]} | {:error, term()}
  def available_services,
    do:
      {:ok,
       [
         %OpenApi{
           id: "test",
           name: "test",
           content: "",
           metadata: %Metadata{provider: "test", file: "test"}
         }
       ]}

  @spec service(id :: String.t()) :: {:ok, Services.t()} | {:error, term()}
  def service(_id), do: {:error, nil}

  @spec dispatch_events(event :: [Services.Event.t()]) :: :ok | {:error, :timeout | term()}
  def dispatch_events(_events), do: :ok
end
