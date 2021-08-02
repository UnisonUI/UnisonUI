defmodule GitProvider.Git.Command do
  require OK
  import OK, only: [success: 1, failure: 1]

  @git_cmd "git"

  @spec clone(uri :: String.t(), branch :: String.t(), directory :: String.t()) ::
          :ok | {:error, term()}
  def clone(uri, branch, directory) do
    with {:ok, _} <-
           execute_command(
             ~w/clone --branch #{branch} --single-branch --depth 1 #{uri} #{directory}/
           ) do
      :ok
    end
  end

  @spec pull(directory :: String.t()) :: :ok | {:error, term()}
  def pull(directory) do
    with {:ok, _} <- execute_command(~w/pull/, directory) do
      :ok
    end
  end

  @spec hash(branch :: String.t(), directory :: String.t()) ::
          {:ok, String.t()} | {:error, term()}
  def hash(branch, directory) do
    OK.for do
      hash <- execute_command(~w/rev-parse --verify #{branch}/, directory)
    after
      String.trim(hash)
    end
  end

  @spec changes(hash :: String.t(), directory :: String.t()) ::
          {:ok, [String.t()]} | {:error, term()}
  def changes(hash, directory) do
    OK.for do
      changed_files <- execute_command(~w/diff --name-only #{hash} HEAD/, directory)
    after
      changed_files
      |> String.split(~r/\R/, trim: true)
      |> Enum.map(fn file -> directory |> Path.join(file) |> Path.expand() end)
    end
  end

  defp execute_command(args, dir \\ nil),
    do: System.cmd(@git_cmd, args, command_options(dir)) |> handle_system_cmd()

  defp command_options(nil), do: [stderr_to_stdout: true]
  defp command_options(dir), do: command_options(nil) ++ [cd: dir]

  defp handle_system_cmd({result, 0}), do: success(result)
  defp handle_system_cmd({error, _}), do: failure(error)
end
