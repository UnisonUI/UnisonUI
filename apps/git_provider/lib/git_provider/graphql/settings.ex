defmodule GitProvider.GraphQL.Settings do
  @type t :: %__MODULE__{
          api_token: String.t(),
          api_uri: String.t(),
          polling_interval: non_neg_integer(),
          patterns: []
        }
  defstruct [
    :api_token,
    :api_uri,
    polling_interval: 60 * 60 * 1_000,
    patterns: []
  ]

  def from_env(key, default_uri) do
    settings = Application.fetch_env!(:git_provider, key)

    struct(__MODULE__, settings)
    |> Map.update!(:polling_interval, fn
      value when is_integer(value) -> value
      value -> Durex.ms!(value)
    end)
    |> Map.update!(:api_uri, fn
      uri when is_binary(uri) -> uri
      _ -> default_uri
    end)
  end
end
