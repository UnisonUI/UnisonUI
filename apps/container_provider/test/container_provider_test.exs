defmodule ContainerProviderTest do
  use ExUnit.Case
  doctest ContainerProvider

  test "greets the world" do
    assert ContainerProvider.hello() == :world
  end
end
