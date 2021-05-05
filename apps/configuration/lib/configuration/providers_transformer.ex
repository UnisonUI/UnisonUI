defmodule Configuration.ProvidersTransformer do
  use Toml.Transform

  def transform(:providers, providers) when is_list(providers),
    do: providers |> Enum.map(fn provider -> String.to_existing_atom("Elixir." <> provider) end)

  def transform(_, v), do: v
end
