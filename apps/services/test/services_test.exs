defmodule ServicesTest do
  use ExUnit.Case
  import Services

  wait_for_storage do
    {:ok, state}
  end

  setup_all do
    Application.put_env(:services, :storage_backend, FakeStorage)
    :ok
  end

  test "Services.alive?/0" do
    assert Services.alive?() == true
  end

  test "Services.available_services/0" do
    assert Services.available_services() == {:ok, []}
  end

  test "Services.service/1" do
    assert Services.service("id") == {:error, nil}
  end

  test "Services.dispatch_events/1" do
    assert Services.dispatch_events([]) == :ok
  end

  test "Services.wait_for_storage/1" do
    assert handle_continue(:wait_for_storage, []) == {:ok, []}
  end
end
