defmodule GitProvider.Github.ClientTest do
  use ExUnit.Case, async: true
  alias GitProvider.Github.{Client, Connection}
  alias GitProvider.Github.Data.Project
  import Mock

  describe "list_endpoint/2" do
    test "successful call" do
      with_mock Connection,
        call: fn body,
                 [
                   url: "http://localhost",
                   connection_module: _,
                   headers: [Authorization: "bearer token"]
                 ] ->
          page_info =
            if String.contains?(body, "$cursor") do
              %{"hasNextPage" => false}
            else
              %{"hasNextPage" => true, "endCursor" => "someId"}
            end

          {:ok,
           %Neuron.Response{
             status_code: 200,
             body: %{
               "data" => %{
                 "viewer" => %{
                   "repositories" => %{
                     "pageInfo" => page_info,
                     "nodes" => [
                       %{
                         "nameWithOwner" => "test/repo",
                         "url" => "http://localhost/test/repo",
                         "defaultBranchRef" => %{"name" => "main"}
                       }
                     ]
                   }
                 }
               }
             }
           }}
        end do
        assert Client.list_projects("http://localhost", "token") ==
                 {:ok,
                  [
                    %Project{
                      name: "test/repo",
                      url: "http://localhost/test/repo",
                      branch: "main"
                    },
                    %Project{name: "test/repo", url: "http://localhost/test/repo", branch: "main"}
                  ]}
      end
    end

    test "call with an error" do
      with_mock Connection,
        call: fn _body,
                 [
                   url: "http://localhost",
                   connection_module: _,
                   headers: [Authorization: "bearer token"]
                 ] ->
          {:ok,
           %Neuron.Response{
             status_code: 200,
             body: %{
               "errors" => [%{"message" => "error 1"}, %{"message" => "error 2"}]
             }
           }}
        end do
        assert Client.list_projects("http://localhost", "token") ==
                 {:error, "error 1, error 2"}
      end
    end
  end
end
