defmodule ConsulProviderTest do
  use ExUnit.Case
  doctest ConsulProvider

  test "greets the world" do
    assert ConsulProvider.hello() == :world
  end
end
