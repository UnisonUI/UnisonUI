defmodule GitProvider.GraphQL.Client do
  @callback list_projects(endpoint :: String.t(), token :: String.t()) ::
              {:ok, [GitProvider.Github.Data.Project.t()]} | {:error, term()}
end
