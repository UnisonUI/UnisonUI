defmodule ContainerProvider.HttpClientTest do
  use ExUnit.Case

  test "download_file/1" do
    assert ContainerProvider.HttpClient.download_file("unknown_host") == nil
  end
end
