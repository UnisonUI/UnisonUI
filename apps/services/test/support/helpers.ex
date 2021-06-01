defmodule Helpers do
  def connect(nodes) do
    {left, right} = Enum.split(nodes, 1)
    pairs(left, right) |> Enum.each(&node_call(&1, :connect))
  end

  def disconnect(nodes, node) do
    {left, right} = Enum.split_with(nodes, &(&1 == node))
    pairs(left, right) |> Enum.each(&node_call(&1, :disconnect))
  end

  defp pairs(left, right), do: Enum.flat_map(left, fn node -> Enum.map(right, &{node, &1}) end)

  defp node_call({node, right}, type), do: :rpc.call(node, Node, type, [right])

  def wait_ready(rem \\ 10)

  def wait_ready(0), do: false

  def wait_ready(rem) do
    if Services.Cluster.running?() do
      true
    else
      Process.sleep(500)
      wait_ready(rem - 1)
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
