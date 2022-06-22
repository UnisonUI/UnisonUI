defmodule GitProvider.Gitlab.Client do
  use GitProvider.GraphQL.Client

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

  @impl true
  def on_success(%{"projects" => projects}) do
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

    {nodes, extract_cursor(projects["pageInfo"])}
  end

  @impl true
  def query(cursor) when is_binary(cursor),
    do:
      {"""
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
       """, %{}}

  def query(_cursor),
    do:
      {"""
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
       """, %{}}
end
