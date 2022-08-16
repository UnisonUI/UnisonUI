defmodule GitProvider.TestServer do
  use Clustering.GlobalServer, supervisor: GitProvider.Git.DynamicSupervisor

  def init(_), do: {:ok, nil}

  def child_spec(name),
    do: %{
      id: {Git, name},
      start: {__MODULE__, :start_link, [[data: nil, name: {Git, name}]]},
      restart: :transient
    }
end
