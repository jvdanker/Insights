import * as d3 from "d3";

function addMouseOver(plot_g, config, data, x, y) {
    const plot_width = config.width - config.margin.left - config.margin.right;
    const plot_height = config.height - config.margin.top - config.margin.bottom;

    let currentDomain = data;
    let currentX = x;
    let currentY = y;

    const mouse_g = plot_g
        .select('mouse_g')
        .datum(data);

    mouse_g
        .append('g')
        .classed('mouse', true)
        .style('display', 'block');

    mouse_g
        .append('rect')
        .attr('width', 2)
        .attr('x',-1)
        .attr('height', plot_height)
        .attr('fill', 'lightgray');

    mouse_g
        .append('circle')
        .attr('r', 3)
        .attr("stroke", "steelblue");

    mouse_g
        .append('text')
        .attr('class', 'mouse-text')
    ;

    const background = plot_g
        .select('mouse-rect')
        .append('rect')
        .attr('width', plot_width)
        .attr('height', plot_height)
        .attr('fill', '#ffffff')
        .attr('fill-opacity', '0');

    background.on("mouseover", () => mouse_g.style('display', 'block'));

    background.on("mouseout", () => mouse_g.style('display', 'none'));

    background.on("mousemove", function(event) {
        const [min, max] = d3.extent(currentDomain);
        const [x_cord,y_cord] = d3.pointer(event);
        const ratio = x_cord / plot_width;
        const currentDateX = new Date(+min + Math.round(ratio * (max - min)));
        const index = d3.bisectCenter(data.map(d => d.epoch), currentDateX);
        const current = data[index];
        const transX = currentX(current.epoch);

        mouse_g
            .attr('transform', `translate(${transX},${0})`);

        mouse_g
            .select('text')
            .text(`${current.epoch.toLocaleDateString()}, ${current.count}`)
            .attr('text-anchor', current.epoch < (min + max) / 2 ? "start" : "end")
            // .attr('x', 0)
            .attr('y', currentY(current.count))
        ;

        mouse_g
            .select('circle')
            .attr('cy', currentY(current.count));
    });

    return {
        mouseDomain: (d) => currentDomain = d,
        setCurrentX: (x) => currentX = x,
        setCurrentY: (y) => currentY = y,
    }
}

export default addMouseOver;