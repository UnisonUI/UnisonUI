defmodule Services.Storage.Raft.ClusterTest do
  use ExUnit.Case

  alias Services.Event
  alias Services.Storage.Raft.Cluster

  setup_all do
    Application.put_env(:services, :raft, nodes: [to_string(node())])
    :ok
  end

  test "starting a cluster" do
    _ = start_supervised!(Cluster)
    assert Cluster.running?()
  end
end
