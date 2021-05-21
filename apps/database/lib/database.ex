defmodule Database do
  @dialyzer [:no_match]
  @typep options :: [
           lock: lock,
           limit: non_neg_integer,
           coerce: boolean
         ]
  @typep lock :: :read | :write | :sticky_write

  defmacrop call(method, arguments) do
    quote(bind_quoted: [fun: method, args: arguments]) do
      apply(:mnesia, fun, args)
    end
  end

  defmacrop call_and_catch(method, arguments) do
    quote(bind_quoted: [fun: method, args: arguments]) do
      try do
        apply(:mnesia, fun, args)
      catch
        :exit, error -> {:error, error}
      end
    end
  end

  @spec transaction(fun, :infinity | non_neg_integer) :: {:ok, any} | {:error, any}
  def transaction(function, retries \\ :infinity) do
    :transaction
    |> call_and_catch([function, retries])
    |> handle_result
  end

  @spec read(module, any, options) :: struct() | nil
  def read(table, id, opts \\ []) do
    lock = Keyword.get(opts, :lock, :read)

    case call(:read, [table, id, lock]) do
      [] -> nil
      [record | _] -> load(record)
    end
  end

  @spec write(struct(), options) :: struct() | no_return
  def write(record = %{__struct__: table}, opts \\ []) do
    tuple = dump(record)
    table = table.name()
    lock = Keyword.get(opts, :lock, :write)

    case call(:write, [table, tuple, lock]) do
      :ok -> record
      term -> term
    end
  end

  @spec delete(module, term, options) :: :ok
  def delete(table, key, opts \\ []) do
    lock = Keyword.get(opts, :lock, :write)
    call(:delete, [table, key, lock])
  end

  @spec delete_record(struct(), options) :: :ok
  def delete_record(record = %{__struct__: table}, opts \\ []) do
    record = dump(record)
    lock = Keyword.get(opts, :lock, :write)

    call(:delete_object, [table.name(), record, lock])
  end

  @spec all(module, options) :: list(struct())
  def all(table, opts \\ []) do
    pattern = table.query_base()
    lock = Keyword.get(opts, :lock, :read)

    :match_object
    |> call([table.name(), pattern, lock])
    |> coerce_records
  end

  def last(table), do: call(:last, [table])

  @result [:"$_"]
  @spec select(module(), list(tuple) | tuple, options) :: list(struct())
  def select(table, guards, opts \\ []) do
    attr_map = table.query_map()
    match_head = table.query_base()
    guards = Database.Query.build(guards, attr_map)

    select_raw(table.name(), [{match_head, guards, @result}], opts)
  end

  @spec select_raw(module(), term, options) :: list(struct()) | list(term)
  def select_raw(table, match_spec, opts \\ []) do
    lock = Keyword.get(opts, :lock, :read)
    limit = Keyword.get(opts, :limit, nil)
    coerce = Keyword.get(opts, :coerce, true)

    args =
      case limit do
        nil -> [table, match_spec, lock]
        limit -> [table, match_spec, limit, lock]
      end

    # Execute select method with the no. of args
    result = call(:select, args)

    # Coerce result conversion if `coerce: true`
    case coerce do
      true -> coerce_records(result)
      false -> result
    end
  end

  defp load(record) when is_tuple(record) do
    [table | values] = Tuple.to_list(record)
    [version | values] = Enum.reverse(values)
    values = Enum.reverse(values)

    table =
      case table.version() do
        ^version ->
          table

        _ ->
          :"#{table}.V#{version}"
      end

    fields = table.attributes() |> Stream.reject(&(&1 == :version)) |> Enum.zip(values)
    struct(table, fields)
  end

  defp dump(%{__struct__: table} = struct) do
    version = table.version()
    struct = struct |> Map.put(:version, version)
    values = table.attributes() |> Enum.map(&Map.get(struct, &1))
    [table.name() | values] |> List.to_tuple()
  end

  defp handle_result(result) do
    case result do
      :ok ->
        :ok

      {:atomic, :ok} ->
        :ok

      {:atomic, term} ->
        {:ok, term}

      {:error, reason} ->
        {:error, reason}

      {:aborted, reason = {exception, data}} ->
        reraise_if_valid!(exception, data)
        {:error, reason}

      {:aborted, reason} ->
        {:error, reason}
    end
  end

  defp reraise_if_valid!(:throw, data), do: throw(data)

  defp reraise_if_valid!(exception, stacktrace) do
    error = Exception.normalize(:error, exception, stacktrace)

    case error do
      # Don't do anything if it's an 'original' ErlangError
      %ErlangError{original: ^exception} ->
        nil

      %{__exception__: true} ->
        reraise(error, stacktrace)

      _ ->
        nil
    end
  end

  # Coerce results when is simple list or tuple
  def coerce_records(records) when is_list(records), do: Enum.map(records, &load/1)

  def coerce_records({records, _term}) when is_list(records), do: coerce_records(records)

  def coerce_records(:"$end_of_table"), do: []
end
