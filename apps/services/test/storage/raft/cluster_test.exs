defmodule Services.Storage.Raft.ClusterTest do
  use ExUnit.Case

  alias Services.Storage.Raft.Cluster

  setup_all do
    Application.put_env(:services, :raft, quorum: 1)
    :ok
  end

  test "starting a cluster" do
    _ = start_supervised!(Cluster)
    assert Cluster.running?()
  end
end
