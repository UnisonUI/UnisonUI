defmodule GitProvider.Gitlab.Client do
  @behaviour GitProvider.GraphQL.Client
  alias GitProvider.GraphQL.Data.Project

  @impl true
  def list_projects(endpoint, token) do
    [_, token] = String.split(token, ":")

    Stream.unfold(nil, fn
      :end ->
        nil

      cursor ->
        cursor |> request(endpoint, token) |> process_response()
    end)
    |> Enum.reduce_while({:ok, []}, fn
      {:error, _} = error, _ -> {:halt, error}
      {:ok, nodes}, {:ok, acc} -> {:cont, {:ok, acc ++ nodes}}
    end)
  end

  defp request(cursor, endpoint, token) do
    cursor
    |> query()
    |> Neuron.query(%{},
      url: endpoint,
      connection_module: GitProvider.GraphQL.Connection,
      headers: [Authorization: "bearer #{token}"]
    )
  end

  defp process_response({:ok, %Neuron.Response{body: %{"data" => %{"projects" => projects}}}}) do
    cursor =
      case projects["pageInfo"] do
        %{"hasNextPage" => false} -> :end
        %{"endCursor" => cursor} -> cursor
      end

    nodes =
      projects["nodes"]
      |> Enum.map(fn %{
                       "fullPath" => name,
                       "webUrl" => url,
                       "repository" => repository
                     } ->
        branch = (repository && repository["rootRef"]) || "master"
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
    do: """
    query {
     projects(membership: true, first: 100, after: "#{cursor}") {
       pageInfo {
         endCursor
         hasNextPage
       }
       nodes {
         fullPath
         webUrl
         repository{
           rootRef
         }
       }
     }
    }
    """

  defp query(_cursor),
    do: """
    query {
     projects(membership: true, first: 100) {
       pageInfo {
         endCursor
         hasNextPage
       }
       nodes {
         fullPath
         webUrl
         repository{
           rootRef
         }
       }
     }
    }
    """
end
