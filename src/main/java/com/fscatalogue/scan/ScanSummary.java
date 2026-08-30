package com.fscatalogue.scan;

public record ScanSummary(int rootsScanned, long directoriesSeen, long filesSeen) {
    ScanSummary plus(ScanSummary other) {
        return new ScanSummary(
                rootsScanned + other.rootsScanned,
                directoriesSeen + other.directoriesSeen,
                filesSeen + other.filesSeen);
    }
}
