defmodule WebhookProviderTest do
  use ExUnit.Case
  doctest WebhookProvider

  test "greets the world" do
    assert WebhookProvider.hello() == :world
  end
end
