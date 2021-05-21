defmodule Database.Schema do
  @dialyzer [:no_match, :no_contracts]
  defmacro __using__(opts) do
    opts = Macro.expand(opts, __CALLER__)

    quote do
      Module.register_attribute(__MODULE__, :versions, accumulate: true)
      Module.register_attribute(__MODULE__, :current, accumulate: false)
      opts = unquote(opts)
      storage = Keyword.get(opts, :storage, :ram_copies)
      type = Keyword.get(opts, :type, :set)
      @storage storage
      @table_type type
      @before_compile Database.Schema
      import Database.Schema
      @dialyzer [:no_match, :no_contracts, :no_fail_call]
    end
  end

  defmacro __before_compile__(env) do
    versions =
      Module.get_attribute(env.module, :versions)
      |> Enum.with_index(1)
      |> Enum.map(fn {kw, index} -> Keyword.put_new(kw, :version, index) end)
      |> Enum.sort_by(&Keyword.get(&1, :version), :desc)

    unless not is_nil(versions) or length(versions) > 0 do
      raise "no structure versions #{inspect(env.module)}"
    end

    [latest_kw | _other] = versions
    attributes = Keyword.get(latest_kw, :attributes, [])

    latest =
      quote do
        defstruct(unquote(attributes))
      end

    if is_nil(Module.get_attribute(env.module, :current)) do
      Module.put_attribute(env.module, :current, latest_kw[:version])
    end

    modules =
      Enum.map(versions, fn kw ->
        current = :"#{env.module}.V#{kw[:version]}"
        attributes = Keyword.get(kw, :attributes, [])
        migration_fn = Keyword.get(kw, :migration, quote(do: fn x -> x end))
        mnesia_attributes = attributes ++ [:version]

        migration =
          if kw[:version] < latest_kw[:version] do
            module =
              {:__aliases__, [alias: false],
               env.module |> to_string |> String.split(".") |> Enum.map(&String.to_atom/1)}

            vars =
              Enum.filter(attributes, &(!match?(:version, &1))) |> Enum.map(&Macro.var(&1, nil))

            params = [
              module
              | vars ++ [kw[:version]]
            ]

            [
              {:def, [],
               [
                 {:migration, [], [{:{}, [], params}]},
                 [
                   do:
                     {:__block__, [],
                      [
                        {:=, [], [Macro.var(:func, nil), migration_fn]},
                        {:=, [],
                         [
                           Macro.var(:result, nil),
                           {:|>, [context: Elixir, import: Kernel],
                            [
                              {{:., [], [{:func, [], nil}]}, [], [{:{}, [], vars}]},
                              {{:., [], [{:__aliases__, [alias: false], [:Tuple]}, :to_list]}, [],
                               []}
                            ]}
                         ]},
                        {:|>, [context: Elixir, import: Kernel],
                         [
                           [
                             {:|, [],
                              [
                                module,
                                {:++, [context: Elixir, import: Kernel],
                                 [Macro.var(:result, nil), [kw[:version] + 1]]}
                              ]}
                           ],
                           {{:., [], [{:__aliases__, [alias: false], [:List]}, :to_tuple]}, [],
                            []}
                         ]}
                      ]}
                 ]
               ]},
              {:def, [],
               [
                 {:next, [], nil},
                 [
                   do:
                     {:__aliases__, [alias: false],
                      (env.module
                       |> to_string
                       |> String.split(".")
                       |> Enum.map(&String.to_atom/1)) ++ [:"V#{kw[:version] + 1}"]}
                 ]
               ]}
            ]
          else
            [
              quote do
                def migration(_), do: nil
              end,
              quote do
                def next, do: nil
              end
            ]
          end

        {:defmodule, [],
         [
           {:__aliases__, [alias: false],
            current
            |> to_string
            |> String.split(".")
            |> Enum.map(&String.to_atom/1)},
           [
             do:
               {:__block__, [],
                [
                  quote do
                    defstruct(unquote(attributes))
                  end,
                  quote do
                    def name, do: unquote(env.module)
                  end,
                  quote do
                    def attributes, do: unquote(mnesia_attributes)
                  end,
                  quote do
                    def version, do: unquote(kw[:version])
                  end,
                  quote do
                    def index, do: unquote(kw[:index])
                  end,
                  quote do
                    def query_map,
                      do:
                        unquote(
                          mnesia_attributes
                          |> Enum.reduce({%{}, 1}, fn attr, {map, position} when is_atom(attr) ->
                            {
                              Map.put(map, attr, :"$#{position}"),
                              position + 1
                            }
                          end)
                          |> elem(0)
                          |> Macro.escape()
                        )
                  end,
                  quote do
                    def query_base,
                      do:
                        unquote(
                          List.to_tuple([
                            env.module
                            | mnesia_attributes
                              |> Enum.count()
                              |> Range.new(1)
                              |> Enum.reverse()
                              |> Enum.map(&:"$#{&1}")
                          ])
                          |> Macro.escape()
                        )
                  end,
                  quote do
                    def element(record, attr) do
                      with index when not is_nil(index) <-
                             Enum.find_index(unquote(mnesia_attributes), &(&1 == attr)) do
                        elem(record, index + 1)
                      end
                    end
                  end
                  | migration
                ]}
           ]
         ]}
      end)

    latest_module = :"#{env.module}.V#{Module.get_attribute(env.module, :current)}"

    functions =
      quote do
        def initialised? do
          match?(
            {:atomic, _},
            :mnesia.transaction(fn ->
              :mnesia.table_info(unquote(env.module), :active_replicas)
            end)
          )
        end

        def init_store do
          store_options = [
            {@storage, [node()]},
            type: @table_type,
            record_name: unquote(env.module),
            attributes: unquote(attributes ++ [:version])
          ]

          store_options =
            case unquote(latest_kw[:index]) do
              nil -> store_options
              index -> Keyword.put(store_options, :index, index)
            end

          :mnesia.create_table(unquote(env.module), store_options)
        end

        @spec copy_store(node()) :: no_return()
        def copy_store(node) do
          _ =
            :mnesia.add_table_copy(
              unquote(env.module),
              node,
              @storage
            )

          :ok
        end

        defdelegate name, to: unquote(latest_module)
        defdelegate query_base, to: unquote(latest_module)
        defdelegate query_map, to: unquote(latest_module)
        defdelegate version, to: unquote(latest_module)
        defdelegate attributes, to: unquote(latest_module)
        defdelegate index, to: unquote(latest_module)
        defdelegate element(record, attr), to: unquote(latest_module)

        def from_version(version), do: :"#{unquote(env.module)}.V#{version}"
      end

    [latest, functions | modules] |> List.flatten()
  end

  defmacro version(do: block) do
    block = _expand(block, __CALLER__)

    quote do
      @versions unquote(block)
    end
  end

  defmacro version(version, do: block) do
    block = _expand(block, __CALLER__) |> Keyword.put(:version, version)

    quote do
      @versions unquote(block)
    end
  end

  defmacro attributes(attributes), do: quote(do: [attributes: unquote(attributes)])
  defmacro index(index), do: quote(do: [index: unquote(index)])

  defmacro migration(do: migration) do
    migration =
      case migration do
        [{:->, _, _}] -> {:fn, [], migration}
        _ -> migration
      end

    quote(do: [migration: unquote(Macro.escape(migration))])
  end

  def _expand(block, caller) do
    case block do
      {:__block__, _, block} ->
        block = Enum.map(block, &Macro.expand(&1, caller))

        unless Enum.all?(block, &match?([{k, _}] when is_atom(k), &1)) do
          raise "not valid"
        end

        Enum.reduce(block, [], &Keyword.merge/2)

      {_, _, _} ->
        Macro.expand(block, caller)

      _ ->
        unless Keyword.keyword?(block) do
          raise("not valid")
        end

        block
    end
  end
end
