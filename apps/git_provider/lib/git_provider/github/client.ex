defmodule GitProvider.Github.Client do
  alias GitProvider.Github.Data.Project

  @spec list_projects(endpoint :: String.t(), token :: String.t()) ::
          {:ok, [GitProvider.Github.Data.Project.t()]} | {:error, term()}
  def list_projects(endpoint, token) do
    Stream.unfold(nil, fn
      :end ->
        nil

      cursor ->
        cursor |> request_github(endpoint, token) |> process_response()
    end)
    |> Enum.reduce_while({:ok, []}, fn
      {:error, _} = error, _ -> {:halt, error}
      {:ok, nodes}, {:ok, acc} -> {:cont, {:ok, acc ++ nodes}}
    end)
  end

  defp request_github(cursor, endpoint, token) do
    {query, variables} = query(cursor)

    Neuron.query(query, variables,
      url: endpoint,
      connection_module: GitProvider.Github.Connection,
      headers: [Authorization: "bearer #{token}"]
    )
  end

  defp process_response(
         {:ok,
          %Neuron.Response{body: %{"data" => %{"viewer" => %{"repositories" => repositories}}}}}
       ) do
    cursor =
      case repositories["pageInfo"] do
        %{"hasNextPage" => false} -> :end
        %{"endCursor" => cursor} -> cursor
      end

    nodes =
      repositories["nodes"]
      |> Enum.map(fn %{
                       "nameWithOwner" => name,
                       "url" => url,
                       "defaultBranchRef" => defaultBranchRef
                     } ->
        branch = if is_nil(defaultBranchRef), do: "master", else: defaultBranchRef["name"]
        %Project{name: name, url: url, branch: branch}
      end)

    {{:ok, nodes}, cursor}
  end

  defp process_response({_, %Neuron.Response{body: %{"message" => error}}}),
    do: {{:error, error}, :end}

  defp process_response({_, %Neuron.Response{body: %{"errors" => errors}}}),
    do: {{:error, errors |> Enum.map(& &1["message"]) |> Enum.join(", ")}, :end}

  defp process_response({:error, error}),
    do: {{:error, Exception.message(error)}, :end}

  defp query(cursor) when is_binary(cursor),
    do:
      {"""
        query($cursor: String!){
         viewer {
           repositories(after: $cursor, first: 100) {
             pageInfo {
              endCursor
              hasNextPage
             }
             nodes {
               nameWithOwner
               url
               defaultBranchRef {
                name
               }
             }
           }
         }
       }
       """, %{"cursor" => cursor}}

  defp query(_cursor),
    do:
      {"""
       {
       viewer {
         repositories(first: 100) {
           pageInfo {
            endCursor
            hasNextPage
           }
           nodes {
             nameWithOwner
             url
             defaultBranchRef {
              name
             }
           }
         }
        }
       }
       """, %{}}
end
