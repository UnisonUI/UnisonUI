defmodule GitProvider.Github.Client do
  use GitProvider.GraphQL.Client

  @impl true
  def list_projects(endpoint, token) do
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
  def on_success(%{"viewer" => %{"repositories" => repositories}}) do
    nodes =
      repositories["nodes"]
      |> Enum.map(fn %{
                       "nameWithOwner" => name,
                       "url" => url,
                       "defaultBranchRef" => defaultBranchRef
                     } ->
        branch = (defaultBranchRef && defaultBranchRef["name"]) || "master"
        %Project{name: name, url: url, branch: branch}
      end)

    {nodes, extract_cursor(repositories["pageInfo"])}
  end

  @impl true
  def query(cursor) when is_binary(cursor),
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

  def query(_cursor),
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
