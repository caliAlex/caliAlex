#version 430

layout (location = 0) in vec3 position;

out vec3 tc;

uniform mat4 sky_v_matrix;
uniform mat4 sky_proj_matrix;
layout (binding = 0) uniform samplerCube samp;

void main(void)
{
	//Texture coordinates are simply the vertex coordinates
	tc = position;

	//Removes transloation from model-view matrix
	mat4 v3_matrix = mat4(mat3(sky_v_matrix));

	gl_Position = sky_proj_matrix * v3_matrix * vec4(position,1.0);
}