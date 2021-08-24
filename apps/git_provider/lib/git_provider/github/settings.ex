defmodule GitProvider.Github.Settings do
  @type t :: %__MODULE__{
          api_token: String.t(),
          api_uri: String.t(),
          polling_interval: non_neg_integer(),
          patterns: []
        }
  defstruct [
    :api_token,
    api_uri: "https://api.github.com/graphql",
    polling_interval: 60 * 60 * 1_000,
    patterns: []
  ]
end
