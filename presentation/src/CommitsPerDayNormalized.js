import React, {useEffect, useRef, useState} from 'react';
import * as d3 from "d3";

function getBusinessDatesCount(startDate, endDate) {
    let count = 0;
    const curDate = new Date(startDate.getTime());
    while (curDate <= endDate) {
        const dayOfWeek = curDate.getDay();
        if(dayOfWeek !== 0 && dayOfWeek !== 6) count++;
        curDate.setDate(curDate.getDate() + 1);
    }
    return count;
}

function drawChart(data) {
    const width = 1300;
    const height = 800;
    const padding = {top: 10, bottom: 50, left: 40, right: 20};

    const min = d3.min(data, d => d.epoch);
    const max = d3.max(data, d => d.epoch);

    const svg = d3
        .select("svg")
        .attr("width", width + padding.right + padding.left)
        .attr("height", height + padding.top + padding.bottom)

    const plotArea = svg
        .append("g")
        .attr("transform","translate("+[padding.left,padding.top]+")")

    const clippingRect = plotArea
        .append("clipPath")
        .attr("id", "clippy")
        .append("rect")
        .attr("width",width)
        .attr("height",height)
        .attr("fill","none")

    const x = d3.scaleTime()
        .range([0, width])
        .domain([min, max]);
    let xx = x.copy();

    const y = d3.scaleLinear()
        .range([height,0])
        .domain([0, d3.max(data, d => d.count)]);

    const y2 = d3.scaleLinear()
        .range([0, height/4])
        .domain([0, d3.max(data, d => d.committers)]);

    const line = d3.line()
        .x(d => xx(d.epoch))
        .y(d => y(d.count))
        .curve(d3.curveMonotoneX);

    const line3 = d3.line()
        .x(d => xx(d.epoch))
        .y(d => y(d.countNormalized))
        .curve(d3.curveMonotoneX);

    const xAxis = d3.axisBottom(xx);
    const xAxisG = plotArea
        .append("g")
        .attr("transform","translate("+[0,height]+")")
        .call(xAxis);

    const yAxis = d3.axisLeft(y);
    const yAxisG = plotArea
        .append("g")
        .call(yAxis)

    const path3 = plotArea
        .append("path")
        .data([data])
        .attr("d", line3)
        .attr("clip-path","url(#clippy)")
        .attr("fill", "none")
        .attr("stroke", "#112944")
        .attr("opaque", 0.3)
        .attr("stroke-width", 2);

    const path = plotArea
        .append("path")
        .data([data])
        .attr("d", line)
        .attr("clip-path","url(#clippy)")
        .attr("fill", "none")
        .attr("stroke", "#707f8d")

    const zoom = d3.zoom()
        .on("zoom", function(event) {
            xx = event.transform.rescaleX(x);
            xAxisG.call(xAxis.scale(xx));

            path.attr("d", line);
            path3.attr("d", line3);
        })

    svg.call(zoom);
}

function CommitsPerDayNormalized() {
    const [firstCommit, setFirstCommit] = useState(0);
    const [totalCommits, setTotalCommits] = useState(0);
    const [dailyAverage, setDailyAverage] = useState(0);
    const [median, setMedian] = useState(0);
    const [businessDays, setBusinessDays] = useState(0);
    const [mostActiveDayOfWeek, setMostActiveDayOfWeek] = useState(0);
    const [mostActiveDayOfWeekCount, setMostActiveDayOfWeekCount] = useState(0);
    const [dailyAverageOnMostActiveDay, setDailyAverageOnMostActiveDay] = useState(0);
    const initialized = useRef(false);

    useEffect( () => {
        if (initialized.current === true) return;
        initialized.current = true;

        d3.csv("commits-per-day.csv").then(data => {
            const data2 = data
                .map(d => {
                    return {
                        epoch: new Date((+d.epoch) * 86400000),
                        count: +d.count,
                        committers: +d.committers,
                        countNormalized: +d.count / +d.committers,
                        files: +d.files
                    }
                });
                // .splice(0, 5);

            const days = data2
                .map(d => {
                    return {...d, dow: d.day = d.epoch.getDay()};
                })
                .reduce((acc, curr) =>
                    acc.set(curr.dow, (acc.get(curr.dow) || 0) + curr.count)
                , new Map());

            const sortedDays = new Map(
                    [...days.entries()].sort((a,b) => b[1] - a[1])
            );

            const [mostActiveDow] = sortedDays.keys();
            const mostActiveDowCount = days.get(mostActiveDow);

            const min = d3.min(data2, d => d.epoch);
            const max = d3.max(data2, d => d.epoch);

            const totalCommits = d3.sum(data2, d => d.count);
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

            drawChart(data2);
        });
    }, []);

    return (
        <>
            <svg></svg>

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

export default CommitsPerDayNormalized;