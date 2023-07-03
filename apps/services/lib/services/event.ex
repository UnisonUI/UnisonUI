defmodule Services.Event do
  @type t :: Services.Event.Up.t() | Services.Event.Down.t() | Services.Event.Changed.t()
  defprotocol From do
    @spec from(input :: term()) :: Services.Event.t()
    def from(input)
  end

  defmodule Up do
    @type t :: %__MODULE__{service: Services.t()}
    @enforce_keys [:service]
    defstruct [:service]
  end

  defmodule Down do
    @type t :: %__MODULE__{id: String.t()}
    @enforce_keys [:id]
    defstruct [:id]
  end

  defmodule Changed do
    @type t :: %__MODULE__{service: Services.t()}
    @enforce_keys [:service]
    defstruct [:service]
  end

  def from(input), do: Services.Event.From.from(input)
end
