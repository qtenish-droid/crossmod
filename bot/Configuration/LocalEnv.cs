namespace CrossMod.Bot.Configuration;

/// <summary>
/// Reads the settings of a local file, so the token does not have to be typed
/// into a terminal and never ends up in a chat or in a history.
///
/// <para>The file is called {@code local.env} and sits next to the project. It
/// is ignored by git and by the Docker build, so it stays on the machine.
/// Every line is a key and a value, the lines that start with a hash are
/// comments:</para>
///
/// <pre>
/// # the token of the bot
/// CROSSMOD_DISCORD_TOKEN=MTU2...
/// CROSSMOD_API_KEY=a-long-random-string
/// CROSSMOD_GUILD_ID=123456789012345678
/// </pre>
/// </summary>
public static class LocalEnv {

	/** The name of the file. */
	public const string FILE_NAME = "local.env";

	/**
	 * Reads the file, if there is one.
	 *
	 * @return the settings of the file, empty when there is no file
	 */
	public static Dictionary<string, string> Load() {
		foreach (string directory in Directories()) {
			string path = Path.Combine(directory, FILE_NAME);

			if (File.Exists(path)) {
				return Parse(path);
			}
		}

		return new Dictionary<string, string>();
	}

	/**
	 * Returns the folders that are searched for the file.
	 *
	 * @return the folders
	 */
	private static IEnumerable<string> Directories() {
		HashSet<string> directories = new();

		try {
			directories.Add(Directory.GetCurrentDirectory());
		} catch (Exception) {
			// The working directory is not readable, the other places are tried.
		}

		try {
			directories.Add(AppContext.BaseDirectory);
		} catch (Exception) {
			// The base directory is not readable, the working directory is used.
		}

		return directories;
	}

	/**
	 * Reads the key value pairs of the file.
	 *
	 * @param path the file
	 * @return the settings of the file
	 */
	private static Dictionary<string, string> Parse(string path) {
		Dictionary<string, string> values = new();

		try {
			foreach (string line in File.ReadAllLines(path)) {
				string trimmed = line.Trim();

				if (trimmed.Length == 0 || trimmed.StartsWith('#')) {
					continue;
				}

				int separator = trimmed.IndexOf('=');

				if (separator <= 0) {
					continue;
				}

				string key = trimmed[..separator].Trim();
				string value = trimmed[(separator + 1)..].Trim();

				if (value.Length >= 2 && value[0] == '"' && value[^1] == '"') {
					value = value[1..^1];
				}

				if (key.Length > 0) {
					values[key] = value;
				}
			}
		} catch (Exception exception) {
			Console.Error.WriteLine($"The file {path} could not be read: {exception.Message}");
		}

		return values;
	}
}
