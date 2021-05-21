defmodule Database.Schema.Events do
  use Database.Schema, storage: :disc_copies, type: :ordered_set
  require Database

  version do
    attributes([:id, :event])
  end

  def insert(event) do
    Database.transaction(fn ->
      last =
        case :mnesia.last(__MODULE__) do
          :"$end_of_table" -> 1
          last -> last
        end

      data = %{__struct__: __MODULE__, id: last + 1, event: event}
      Database.write(data)
    end)
  end

  def all, do: Database.all(__MODULE__)

  def all_after(id), do: Database.select(__MODULE__, [{:>, :id, id}])

  def all_before(id), do: Database.select(__MODULE__, [{:<=, :id, id}])
end
