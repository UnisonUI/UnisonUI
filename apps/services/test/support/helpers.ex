defmodule Helpers do
  def wait_ready(rem \\ 10)

  def wait_ready(0), do: false

  def wait_ready(rem) do
    %{servers: servers} = :ra.overview()

    if Map.has_key?(servers, :unisonui) do
      true
    else
      Process.sleep(500)
      wait_ready(rem - 1)
    end
  end

  def get_leaders(nodes, rem \\ 10)
  def get_leaders(nodes, 0), do: {[], nodes}

  def get_leaders(nodes, rem) do
    {left, right} =
      nodes
      |> Enum.split_with(fn node ->
        %{servers: %{unisonui: %{state: state}}} = :rpc.call(node, :ra, :overview, [])
        state == :leader
      end)

    if left == [] do
      Process.sleep(500)
      get_leaders(nodes, rem - 1)
    else
      {left, right}
    end
  end

  def wait_data(rem \\ 10)
  def wait_data(0), do: []

  def wait_data(rem) do
    case Consumer.get_state() do
      [] ->
        Process.sleep(500)
        wait_data(rem - 1)

      data ->
        data
    end
  end

  def get_state do
    case wait_data() do
      [] ->
        :error

      data ->
        Consumer.reset_state()
        {:ok, data}
    end
  end
end
