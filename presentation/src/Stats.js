import React, {useEffect, useState} from "react";
import * as d3 from "d3";

function getBusinessDatesCount(startDate, endDate) {
    if (typeof startDate === 'undefined') return;

    let count = 0;
    const curDate = new Date(startDate.getTime());
    while (curDate <= endDate) {
        const dayOfWeek = curDate.getDay();
        if(dayOfWeek !== 0 && dayOfWeek !== 6) count++;
        curDate.setDate(curDate.getDate() + 1);
    }
    return count;
}

export function Stats({data}) {
    const [firstCommit, setFirstCommit] = useState(0);
    const [totalCommits, setTotalCommits] = useState(0);
    const [dailyAverage, setDailyAverage] = useState(0);
    const [median, setMedian] = useState(0);
    const [businessDays, setBusinessDays] = useState(0);
    const [mostActiveDayOfWeek, setMostActiveDayOfWeek] = useState(0);
    const [mostActiveDayOfWeekCount, setMostActiveDayOfWeekCount] = useState(0);
    const [dailyAverageOnMostActiveDay, setDailyAverageOnMostActiveDay] = useState(0);

    useEffect(() => {
        if (typeof data === 'undefined' || data.length === 0) return;

        const days = data
            .map(d => {
                return {...d, dow: d.day = d.epoch.getDay()};
            })
            .reduce((acc, curr) =>
                    acc.set(curr.dow, (acc.get(curr.dow) || 0) + curr.count)
                , new Map());

        const sortedDays = new Map(
            [...days.entries()].sort((a, b) => b[1] - a[1])
        );

        const [mostActiveDow] = sortedDays.keys();
        const mostActiveDowCount = days.get(mostActiveDow);

        const min = d3.min(data, d => d.epoch);
        const max = d3.max(data, d => d.epoch);

        const totalCommits = d3.sum(data, d => d.count);
        const businessDays = getBusinessDatesCount(min, max);

        setBusinessDays(businessDays);
        setFirstCommit(min.toLocaleString());
        setTotalCommits(totalCommits);
        setDailyAverage(Math.ceil(totalCommits / businessDays));
        setMedian(d3.median(data, d => d.count));
        setMostActiveDayOfWeek(
            ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'][mostActiveDow]
        );
        setMostActiveDayOfWeekCount(mostActiveDowCount);
        setDailyAverageOnMostActiveDay(Math.ceil(totalCommits / businessDays / 5));
    }, [data]);

    return (
        <>
            <div>First commit on: <span>{firstCommit}</span></div>
            <div>Total number of commits: {totalCommits} ({businessDays} business days)</div>
            <div>Average number of commits: {dailyAverage}</div>
            <div>Median number of commits: {median}</div>
            <div>Most active day of the week <b>{mostActiveDayOfWeek}</b></div>
            <div>Total number of commits on the most active day of the week: {mostActiveDayOfWeekCount}</div>
            <div>Average number of commits on the most active day of the week: {dailyAverageOnMostActiveDay}</div>
        </>
    );
}