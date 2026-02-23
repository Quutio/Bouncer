using System.Runtime.InteropServices;
using Bouncer.Server.Server.Sort;

namespace Bouncer.Server.Collection;

internal sealed class PlayerCountTracker<T>
	where T : notnull
{
	private readonly Lock globalLock = new();

	private readonly Dictionary<T, TrackEntry> values = [];
	private readonly Dictionary<string, SortedSet<Entry>> byState = [];

	internal Lock Lock => this.globalLock;

	private SortedSet<Entry> GetOrAddNoLock(string type)
	{
		ref SortedSet<Entry>? set = ref CollectionsMarshal.GetValueRefOrAddDefault(this.byState, type, out _);
		set ??= [];

		return set;
	}

	internal void Update(T value, string type, int playerCount)
	{
		lock (this.globalLock)
		{
			ref TrackEntry entry = ref CollectionsMarshal.GetValueRefOrAddDefault(this.values, value, out bool exists);
			if (exists)
			{
				entry.Set.Remove(new Entry(entry.PlayerCount, entry.Timestamp, value));

				if (entry.Type != type)
				{
					entry.Type = type;
					entry.Set = this.GetOrAddNoLock(type);
				}

				if (playerCount <= 0)
				{
					entry.Timestamp = Environment.TickCount64;
				}
			}
			else
			{
				entry.Type = type;
				entry.Set = this.GetOrAddNoLock(type);
				entry.Timestamp = Environment.TickCount64;
			}

			entry.PlayerCount = playerCount;

			entry.Set.Add(new Entry(playerCount, entry.Timestamp, value));
		}
	}

	internal void Remove(T value)
	{
		lock (this.globalLock)
		{
			if (!this.values.Remove(value, out TrackEntry entry))
			{
				return;
			}

			entry.Set.Remove(new Entry(entry.PlayerCount, entry.Timestamp, value));
		}
	}

	internal IEnumerable<T> Sort(IServerSort? sort)
	{
		foreach (Entry entry in this.SortCore(sort))
		{
			yield return entry.Value;
		}
	}

	private IEnumerable<Entry> SortCore(IServerSort? sort)
	{
		SortedSet<Entry> entries = this.byState["bouncer:running"];
		if (sort is null)
		{
			return entries;
		}

		if (sort is ServerPlayerCountSorter playerCountSorter && playerCountSorter.Ascending)
		{
			return entries.Reverse();
		}

		return entries;
	}

	private readonly record struct Entry(int PlayerCount, long Timestamp, T Value) : IComparable<Entry>
	{
		public int CompareTo(Entry other)
			=> (other.PlayerCount, this.Timestamp, this.Value).CompareTo((this.PlayerCount, other.Timestamp, other.Value));
	}

	private record struct TrackEntry(int PlayerCount, long Timestamp, string Type, SortedSet<Entry> Set);
}
