defmodule GitProvider.LocalGit do
  @default_spec "specifications:
    - test
  "
  defstruct [:path]

  defp cmd(cd, args), do: System.cmd("git", args, cd: cd, stderr_to_stdout: true)

  def new do
    tmp_dir = System.tmp_dir!()

    dir =
      "unisonui_" <>
        (:crypto.strong_rand_bytes(16) |> Base.encode32(case: :lower, padding: false))

    dir = Path.join(tmp_dir, dir)
    _ = File.mkdir(dir)
    repo = %__MODULE__{path: dir}
    {_, 0} = cmd(dir, ~w/init/)
    cmd(dir, ~w/config "user.email" "test@test.org"/)
    cmd(dir, ~w/config "user.name" test/)
    commit(repo, "init", "init")
    commit(repo, ".unisonui.yaml", @default_spec)
    repo
  end

  def commit(%__MODULE__{path: repo}, file) do
    cmd(repo, ~w/add #{file}/)
    cmd(repo, ["commit", "-m", "new file"])
  end

  def commit(%__MODULE__{path: path}=repo, file, content) do
    file = Path.join(path, file)
    File.write!(file, content)
    commit(repo, file)
  end

  def rm(%__MODULE__{path: path}, file) do
    cmd(path, ~w/rm #{Path.join(path, file)}/) 
    cmd(path, ["commit", "-m", "rm file"])
  end

  def mv(%__MODULE__{path: path}, old_path, new_path) do
    cmd(path, ["mv", ~s/"#{Path.join(path, old_path)}"/, ~s/"#{Path.join(path, new_path)}"/])
    cmd(path, ["commit", "-m", "mv file"])
  end

  def clean(%__MODULE__{path: path}), do: File.rm_rf!(path)
end
