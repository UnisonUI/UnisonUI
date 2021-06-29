defmodule Clustering.GlobalServer do
  defmacro __using__(opts) do
    supervisor = opts[:supervisor] || Clustering.DynamicSupervisor

    quote do
      use GenServer
      import Clustering.GlobalServer

      def child_spec(opts),
        do: %{
          id: __MODULE__,
          start: {__MODULE__, :start_link, [opts]},
          restart: :transient
      } 

      @spec start_child(any()) :: DynamicSupervisor.on_start_child()
      def start_child(opts),
        do: Horde.DynamicSupervisor.start_child(unquote(supervisor), child_spec(opts))

      def start_link(opts) when not is_list(opts), do: start_link(data: opts)

      def start_link(opts) do
        data = opts[:data]

        name =
          case opts[:name] do
            nil -> via()
            name -> via(name)
          end

        case GenServer.start_link(__MODULE__, data, name: name) do
          {:ok, pid} ->
            {:ok, pid}

          {:error, {:already_started, _}} ->
            :ignore
        end
      end

      defoverridable child_spec: 1
    end
  end

  defmacro via do
    quote do
      {:via, Horde.Registry, {Clustering.Registry, __MODULE__}}
    end
  end

  defmacro via(name) do
    quote do
      {:via, Horde.Registry, {Clustering.Registry, {__MODULE__, unquote(name)}}}
    end
  end
end
