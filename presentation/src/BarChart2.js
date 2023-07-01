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

function BarChart2({data}) {
    const initialized = useRef(false);

    const margin = {top: 20, right: 20, bottom: 30, left: 40};
    const width = 960 - margin.left - margin.right;
    const height = 500 - margin.top - margin.bottom;

    useEffect( () => {
        if (initialized.current === true) return;
        initialized.current = true;

        d3.csv("commits-per-day.csv").then(data => {
            data = data.map(d => {
                return {
                    epoch: new Date((+d.epoch) * 86400000), count: +d.count}
                })
                .filter(d => {
                    const date = new Date();
                    date.setFullYear( date.getFullYear() - 1 );
                    return d.epoch > date;
                });

            const min = d3.min(data, d => d.epoch);
            const max = d3.max(data, d => d.epoch);
            const totalCommits = d3.sum(data, d => d.count);
            const businessDays = getBusinessDatesCount(min, max);

            setBusinessDays(businessDays);
            setFirstCommit(min.toLocaleString());
            setTotalCommits(totalCommits);
            setDailyAverage(Math.round(totalCommits / businessDays));
            setMedian(d3.median(data, d => d.count));

            const xScale = d3.scaleTime()
                .range([0, width])
                .domain([min, max]);

            const yScale = d3.scaleLinear()
                .range([height, 0])
                .domain([0, d3.max(data, (d) => d.count)]);

            const myLine = d3.line()
                .x(d => xScale(d.epoch))
                .y(d => yScale(d.count))
                .curve(d3.curveCardinal);

            const svg = d3
                .select(".chart")
                .append("svg")
                .attr("width", width + margin.left + margin.right)
                .attr("height", height + margin.top + margin.bottom)
                .append("g")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            svg
                .selectAll(".line")
                .data([data])
                .join("path")
                .attr("class", "line")
                .attr("d", myLine(data))
                .attr("fill", "none")
                .attr("stroke", "#454589");

            svg
                .append("g")
                .attr("transform", "translate(0," + height + ")")
                .call(d3.axisBottom(xScale));

            svg
                .append("g")
                .call(d3.axisLeft(yScale));
        });

    }, [data]);

    const [firstCommit, setFirstCommit] = useState(0);
    const [totalCommits, setTotalCommits] = useState(0);
    const [dailyAverage, setDailyAverage] = useState(0);
    const [median, setMedian] = useState(0);
    const [businessDays, setBusinessDays] = useState(0);

    return (
        <>
            <div className="chart"></div>

            <div>First commit on: <span>{firstCommit}</span></div>
            <div>Total number of commits: {totalCommits} ({businessDays} business days)</div>
            <div>Average number of commits {dailyAverage}</div>
            <div>Median number of commits {median}</div>
        </>
    );
}

export default BarChart2;