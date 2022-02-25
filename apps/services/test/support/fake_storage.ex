defmodule FakeStorage do
  @behaviour Services.Storage

  @spec alive?() :: true
  def alive?, do: true

  @spec available_services :: {:ok, [Services.t()]} | {:error, term()}
  def available_services, do: {:ok, []}

  @spec service(id :: String.t()) :: {:ok, Services.t()} | {:error, term()}
  def service(_id), do: {:error, nil}

  @spec dispatch_events(event :: [Services.Event.t()]) :: :ok | {:error, :timeout | term()}
  def dispatch_events(_events), do: :ok
end
