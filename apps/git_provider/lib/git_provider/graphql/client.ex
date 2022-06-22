defmodule GitProvider.GraphQL.Client do
  @callback list_projects(endpoint :: String.t(), token :: String.t()) ::
              {:ok, [GitProvider.Github.Data.Project.t()]} | {:error, term()}

  @callback query(cursor :: String.t() | :end) :: {String.t(), map()}
  @callback on_success(data :: map()) ::
              {[GitProvider.GraphQL.Data.Project.t()], String.t() | :end}

  defmacro __using__(_opts) do
    quote do
      @behaviour GitProvider.GraphQL.Client
      alias GitProvider.GraphQL.Data.Project

      defp request(cursor, endpoint, token) do
        {query, variables} = query(cursor)

        Neuron.query(query, variables,
          url: endpoint,
          connection_module: GitProvider.GraphQL.Connection,
          headers: [Authorization: "bearer #{token}"]
        )
      end

      defp extract_cursor(%{"hasNextPage" => false}), do: :end
      defp extract_cursor(%{"endCursor" => cursor}), do: cursor

      defp process_response({:ok, %Neuron.Response{body: %{"data" => data}}}) do
        {nodes, cursor} = on_success(data)
        {{:ok, nodes}, cursor}
      end

      defp process_response({_, %Neuron.Response{body: %{"message" => error}}}),
        do: {{:error, error}, :end}

      defp process_response({_, %Neuron.Response{body: %{"errors" => errors}}}),
        do: {{:error, errors |> Enum.map(& &1["message"]) |> Enum.join(", ")}, :end}

      defp process_response({:error, error}),
        do: {{:error, Exception.message(error)}, :end}
    end
  end
end
