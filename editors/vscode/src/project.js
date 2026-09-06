'use strict';

// One immutable source snapshot per project generation; at most four response
// entries, including in-flight requests. No data survives a relevant change.
class Project {
  constructor(read, invoke, publish, failure) {
    Object.assign(this, {read, invoke, publish, failure});
    this.generation = 0; this.entries = new Map(); this.snapshot = null; this.disposed = false;
  }
  invalidate() {
    ++this.generation;
    for (const entry of this.entries.values()) { entry.cancelled = true; entry.abort.abort(); }
    this.entries.clear(); this.snapshot = null;
  }
  dispose() { this.disposed = true; this.invalidate(); }
  get(query) {
    if (this.disposed) return Promise.resolve(null);
    const key = JSON.stringify(query || null);
    if (this.entries.has(key)) {
      const entry = this.entries.get(key);
      this.entries.delete(key); this.entries.set(key, entry);
      return entry.promise;
    }
    while (this.entries.size >= 4) {
      const oldest = this.entries.keys().next().value;
      const entry = this.entries.get(oldest); entry.cancelled = true; entry.abort.abort();
      this.entries.delete(oldest);
    }
    const version = this.generation;
    const entry = {abort:new AbortController(),cancelled:false};
    this.entries.set(key,entry);
    entry.promise = (async () => {
      try {
        this.snapshot ||= Promise.resolve().then(() => this.read(version));
        const snapshot = await this.snapshot;
        if (entry.cancelled || this.disposed || version !== this.generation) return null;
        const request = query ? {...snapshot,cursor:query} : snapshot;
        const result = await this.invoke(request, entry.abort.signal);
        if (entry.cancelled || this.disposed || version !== this.generation) return null;
        const state = {snapshot,result,generation:version};
        this.publish(state);
        return state;
      } catch (error) {
        if (!entry.cancelled && !this.disposed && version === this.generation) {
          this.entries.delete(key); this.snapshot = null;
          this.failure(error);
        }
        return null;
      }
    })();
    return entry.promise;
  }
}
module.exports = {Project};
